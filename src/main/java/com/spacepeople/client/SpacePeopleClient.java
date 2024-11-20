package com.spacepeople.client;

import com.spacepeople.model.SpacePeopleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "spacePeopleClient", url = "http://api.open-notify.org")
public interface SpacePeopleClient {
    @GetMapping("/astros.json")
    SpacePeopleResponse getPeopleInSpace();
}
