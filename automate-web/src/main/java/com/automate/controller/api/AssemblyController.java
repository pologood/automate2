package com.automate.controller.api;

import com.alibaba.fastjson.JSON;
import com.automate.common.ResponseEntity;
import com.automate.entity.AssemblyLineEntity;
import com.automate.service.AssemblyLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author: genx
 * @date: 2019/2/7 19:41
 */
@Controller
@RequestMapping("/api/assembly")
public class AssemblyController {

    @Autowired
    private AssemblyLineService assemblyLineService;

    @ResponseBody
    @RequestMapping(value="/assemblyLine", method = RequestMethod.POST)
    public ResponseEntity<String> createAssemblyLine(AssemblyLineEntity assemblyLineEntity) {
        System.out.println(JSON.toJSONString(assemblyLineEntity));
        assemblyLineService.save(assemblyLineEntity);
        return ResponseEntity.ok(null);
    }
}