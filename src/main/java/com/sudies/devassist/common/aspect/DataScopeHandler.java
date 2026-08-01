package com.sudies.devassist.common.aspect;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.sudies.devassist.common.enums.RoleCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.project.entity.ProjectMember;
import com.sudies.devassist.modules.project.mapper.ProjectMemberMapper;
import jakarta.annotation.Resource;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据范围 SQL 处理器（基于 MP MultiDataPermissionHandler）。
 * <p>仅对 @DataScope 标注的方法（ThreadLocal 有列名）生效：
 * <ul>
 *   <li>ADMIN：不追加条件（全局）</li>
 *   <li>其他角色：project_id IN (当前用户经 project_member 可见的项目)；无项目则 1=0 空集</li>
 * </ul>
 * 对应关键流程详细设计 §1.3。
 */
@Component
public class DataScopeHandler implements MultiDataPermissionHandler {

    @Resource
    @Lazy
    private ProjectMemberMapper projectMemberMapper;

    @Override
    public Expression getSqlSegment(Table table, Expression where, String whereSegment) {
        String column = DataScopeContext.get();
        if (column == null) {
            return null;
        }
        // 立即清除：避免内部 project_member 查询再次进入本拦截器造成递归栈溢出
        DataScopeContext.clear();
        Long uid = SecurityUtils.currentUserIdOrNull();
        if (uid == null) {
            return null;
        }
        if (SecurityUtils.hasRole(RoleCode.ADMIN.name())) {
            return null;
        }
        List<Long> projectIds = projectMemberMapper.selectList(
                        Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getUserId, uid))
                .stream().map(ProjectMember::getProjectId).distinct().toList();
        Alias alias = table.getAlias();
        String qualified = (alias != null && alias.getName() != null) ? alias.getName() + "." + column : column;
        if (projectIds.isEmpty()) {
            return new EqualsTo(new Column(qualified), new LongValue(-1L));
        }
        // 实际 jsqlparser 为 5.2（非 HANDOFF 旧记 4.6）：用 ParenthesedExpressionList（自带括号）
        // 构造 IN，生成 project_id IN (1,3)——IN 是原子谓词，与原 WHERE 的 AND 无优先级冲突。
        // 旧 OR 链写法因缺括号在多项目成员时退化为跨项目（踩坑 #29）。
        ParenthesedExpressionList<Expression> list = new ParenthesedExpressionList<>();
        for (Long pid : projectIds) {
            list.addExpression(new LongValue(pid));
        }
        return new InExpression(new Column(qualified), list);
    }
}
