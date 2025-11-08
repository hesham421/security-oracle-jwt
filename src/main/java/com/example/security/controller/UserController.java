package com.example.security.controller;

import com.example.security.dto.CreateUserRequest;
import com.example.security.dto.UserDto;
import com.example.security.mapper.UserMapper;
import com.example.security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
CteInsertStrategy
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_CREATE')")
    public UserDto create(@RequestBody @Valid CreateUserRequest req){
        return UserMapper.toDto(userService.createUser(req));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_VIEW')")
    public List<UserDto> all(){
        System.out.println("inside all ::::::");
        return userService.listUsers().stream().map(UserMapper::toDto).toList();
    }
}
