package com.example.demo.controller;

import com.example.demo.entity.db.Item;
import com.example.demo.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

//返回一个json数据给前端
@Controller
public class SearchController {

    @Autowired
    private GameService gameService;
    //定义后端如何处理前端发送来的请求
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    //convert map to json
    @ResponseBody
    public Map<String, List<Item>> search(@RequestParam(value = "game_id") String gameId) {
        return gameService.searchItems(gameId);
    }
}
