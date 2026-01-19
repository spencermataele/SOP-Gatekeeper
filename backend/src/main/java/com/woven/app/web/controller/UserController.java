package com.woven.app.web.controller;

import com.woven.app.repository.UserRepository;
import com.woven.app.service.user.UsersService;
import com.woven.app.web.dto.admin.UserCreateDto;
import com.woven.app.web.dto.admin.UserDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UsersService usersService;

    public UserController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    public List<UserDto> list() {
        return usersService.list();
    }

    @PostMapping
    public UserDto create(@Valid @RequestBody UserCreateDto dto) {
        return usersService.create(dto);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Integer id, @Valid @RequestBody UserCreateDto dto) {
        return usersService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        usersService.delete(id);
    }

}
