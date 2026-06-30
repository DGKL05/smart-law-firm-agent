package com.example.dgkl.module.common;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public abstract class AbstractUserCrudController<T extends BaseEntity & UserOwned> {
    @org.springframework.beans.factory.annotation.Autowired
    private UserCrudService userCrudService;

    protected abstract IService<T> service();

    protected List<String> keywordColumns() {
        return List.of("title");
    }

    @GetMapping
    public Result<PageResult<T>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(userCrudService.page(service(), pageNum, pageSize, keyword, keywordColumns()));
    }

    @PostMapping
    public Result<T> create(@RequestBody T entity) {
        return Result.success(userCrudService.create(service(), entity));
    }

    @PutMapping("/{id}")
    public Result<T> update(@PathVariable Long id, @RequestBody T entity) {
        return Result.success(userCrudService.update(service(), id, entity));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userCrudService.delete(service(), id);
        return Result.success();
    }

    protected T requireOwnRecord(Long id) {
        return userCrudService.requireOwnRecord(service(), id);
    }

    protected Long requireUserId() {
        return userCrudService.requireUserId();
    }
}
