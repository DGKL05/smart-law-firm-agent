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

public abstract class AbstractAdminCrudController<T extends BaseEntity> {
    @org.springframework.beans.factory.annotation.Autowired
    private AdminCrudService adminCrudService;

    protected abstract IService<T> service();

    protected List<String> keywordColumns() {
        return List.of("title", "name");
    }

    @GetMapping
    public Result<PageResult<T>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(adminCrudService.page(service(), pageNum, pageSize, keyword, keywordColumns()));
    }

    @GetMapping("/{id}")
    public Result<T> detail(@PathVariable Long id) {
        return Result.success(adminCrudService.detail(service(), id));
    }

    @PostMapping
    public Result<T> create(@RequestBody T entity) {
        return Result.success(adminCrudService.create(service(), entity));
    }

    @PutMapping("/{id}")
    public Result<T> update(@PathVariable Long id, @RequestBody T entity) {
        return Result.success(adminCrudService.update(service(), id, entity));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCrudService.delete(service(), id);
        return Result.success();
    }
}
