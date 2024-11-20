package com.spacepeople.controller;

import com.spacepeople.service.SpacePeopleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SpacePeopleController {
    private final SpacePeopleService spacePeopleService;

    @GetMapping("/space-people")
    public void getSpacePeople() {
        spacePeopleService.executeAllClients();
    }
}
