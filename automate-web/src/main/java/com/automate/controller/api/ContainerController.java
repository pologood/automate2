package com.automate.controller.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.automate.common.ResponseEntity;
import com.automate.common.utils.BeanUtils;
import com.automate.controller.BaseController;
import com.automate.entity.ContainerEntity;
import com.automate.entity.ServerEntity;
import com.automate.exec.ExecCommand;
import com.automate.service.ContainerService;
import com.automate.service.ContainerTypeService;
import com.automate.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author: genx
 * @date: 2019/2/27 20:28
 */
@RestController
@RequestMapping("/api")
public class ContainerController extends BaseController {

    @Autowired
    private ContainerService containerService;

    @Autowired
    private ContainerTypeService containerTypeService;

    @RequestMapping(value = "/containers", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity<JSONArray> containers() {
        Iterable<ContainerEntity> list = containerService.findAll();
        JSONArray array = new JSONArray();
        JSONObject item;
        ServerEntity serverEntity;
        for (ContainerEntity containerEntity : list) {
            item = containerEntity.toJson();
            serverEntity = ServerService.getModelByCache(containerEntity.getServerId());
            if (serverEntity != null) {
                item.put("serverName", serverEntity.getName());
                item.put("serverRemark", serverEntity.getRemark());
            }
            array.add(item);
        }
        return ResponseEntity.ok(array);
    }

    @RequestMapping(value = "/container", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public ResponseEntity createContainer(ContainerEntity model) {
        logger.debug(JSON.toJSONString(model));
        containerService.save(model);
        return ResponseEntity.ok();
    }

    @RequestMapping(value = "/container", method = RequestMethod.PUT, produces = "application/json;charset=UTF-8")
    public ResponseEntity updateContainer(ContainerEntity model) {
        if (model.getId() == null) {
            return ResponseEntity.of(HttpStatus.BAD_REQUEST, "id is required");
        }
        logger.debug(JSON.toJSONString(model));
        containerService.save(model);
        return ResponseEntity.ok();
    }


    @RequestMapping(value = "/container/{id}", method = RequestMethod.PATCH, produces = "application/json;charset=UTF-8")
    public ResponseEntity patchContainer(@PathVariable(value = "id") Integer id, ContainerEntity model) {
        ContainerEntity containerEntity = getContainerEntitySafe(id);

        BeanUtils.copyPropertiesIgnoreNull(model, containerEntity, "id");
        containerService.save(containerEntity);
        return ResponseEntity.ok();
    }

    @RequestMapping(value = "/container/{id}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity<JSONObject> container(@PathVariable(value = "id") Integer id) {
        return ResponseEntity.ok(getContainerEntitySafe(id).toJson());
    }


    /**
     * 查询容器状态
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/container/{id}/check", produces = "application/json;charset=UTF-8")
    public ResponseEntity containerCheck(@PathVariable(value = "id") Integer id) throws Exception {
        ContainerEntity containerEntity = getContainerEntitySafe(id);
        ExecCommand execCommand = containerService.containerCheck(containerEntity);
        if (execCommand.getExitValue() == 0) {
            return ResponseEntity.of(HttpStatus.OK, "该容器正在运行中");
        } else if (execCommand.getExitValue() == 3) {
            return ResponseEntity.of(HttpStatus.NO_CONTENT, "该容器未运行");
        } else {
            return ResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, "exit=" + execCommand.getExitValue());
        }
    }

    /**
     * 启动容器
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/container/{id}/start", produces = "application/json;charset=UTF-8")
    public ResponseEntity containerStart(@PathVariable(value = "id") Integer id) throws Exception {
        ContainerEntity containerEntity = getContainerEntitySafe(id);

        ExecCommand execCommand = containerService.containerStart(containerEntity);
        if (execCommand.getExitValue() == 0) {
            return ResponseEntity.of(HttpStatus.OK, "启动成功");
        } else if (execCommand.getExitValue() == 2) {
            return ResponseEntity.of(HttpStatus.OK, "容器正在运行,请勿重复启动");
        } else {
            return ResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, "exit=" + execCommand.getExitValue());
        }
    }

    /**
     * 关闭容器
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/container/{id}/stop", produces = "application/json;charset=UTF-8")
    public ResponseEntity containerStop(@PathVariable(value = "id") Integer id) throws Exception {
        ContainerEntity containerEntity = getContainerEntitySafe(id);
        ExecCommand execCommand = containerService.containerStop(containerEntity);
        if (execCommand.getExitValue() == 0) {
            return ResponseEntity.of(HttpStatus.OK, "关闭成功");
        } else {
            return ResponseEntity.of(HttpStatus.INTERNAL_SERVER_ERROR, "exit=" + execCommand.getExitValue());
        }
    }

    private ContainerEntity getContainerEntitySafe(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id is required");
        }
        Optional<ContainerEntity> containerEntity = containerService.getModel(id);
        if (!containerEntity.isPresent()) {
            throw new IllegalArgumentException("未找到相应容器");
        }
        if (containerEntity.get().getServerId() == null || containerEntity.get().getServerId() <= 0) {
            throw new IllegalArgumentException("当前容器未绑定服务器");
        }
        return containerEntity.get();
    }
}
