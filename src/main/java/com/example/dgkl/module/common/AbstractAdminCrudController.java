package com.example.dgkl.module.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

public abstract class AbstractAdminCrudController<T extends BaseEntity> {
    protected abstract IService<T> service();

    protected QueryWrapper<T> pageQuery(String keyword) {
        QueryWrapper<T> wrapper = new QueryWrapper<T>().orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("title", keyword).or().like("name", keyword);
        }
        return wrapper;
    }

    @GetMapping
    public Result<PageResult<T>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(service().page(new Page<>(pageNum, pageSize), pageQuery(keyword))));
    }

    @GetMapping("/{id}")
    public Result<T> detail(@PathVariable Long id) {
        return Result.success(service().getById(id));
    }

    @PostMapping
    public Result<T> create(@RequestBody T entity) {
        EntityLifecycle.forCreate(entity);
        service().save(entity);
        return Result.success(entity);
    }

    @PutMapping("/{id}")
    public Result<T> update(@PathVariable Long id, @RequestBody T entity) {
        entity.setId(id);
        EntityLifecycle.forUpdate(entity);
        service().updateById(entity);
        return Result.success(service().getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service().removeById(id);
        return Result.success();
    }
}
