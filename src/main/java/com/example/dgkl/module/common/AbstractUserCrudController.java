package com.example.dgkl.module.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dgkl.common.BusinessException;
import com.example.dgkl.common.PageResult;
import com.example.dgkl.common.Result;
import com.example.dgkl.security.SecurityUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class AbstractUserCrudController<T extends BaseEntity & UserOwned> {
    protected abstract IService<T> service();

    protected QueryWrapper<T> userQuery(Long userId, String keyword) {
        QueryWrapper<T> wrapper = new QueryWrapper<T>().eq("user_id", userId).orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("title", keyword);
        }
        return wrapper;
    }

    @GetMapping
    public Result<PageResult<T>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword) {
        Long userId = requireUserId();
        return Result.success(PageResult.of(service().page(new Page<>(pageNum, pageSize), userQuery(userId, keyword))));
    }

    @PostMapping
    public Result<T> create(@RequestBody T entity) {
        entity.setUserId(requireUserId());
        EntityLifecycle.forCreate(entity);
        service().save(entity);
        return Result.success(entity);
    }

    @PutMapping("/{id}")
    public Result<T> update(@PathVariable Long id, @RequestBody T entity) {
        T old = requireOwnRecord(id);
        entity.setId(id);
        entity.setUserId(old.getUserId());
        EntityLifecycle.forUpdate(entity);
        service().updateById(entity);
        return Result.success(service().getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireOwnRecord(id);
        service().removeById(id);
        return Result.success();
    }

    protected T requireOwnRecord(Long id) {
        T entity = service().getById(id);
        Long userId = requireUserId();
        if (entity == null || !userId.equals(entity.getUserId())) {
            throw new BusinessException(403, "只能访问自己的数据");
        }
        return entity;
    }

    protected Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
