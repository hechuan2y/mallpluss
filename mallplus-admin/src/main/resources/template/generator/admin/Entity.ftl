package ${package}.entity;

import lombok.Data;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
<#if hasTimestamp>
import java.sql.Timestamp;
</#if>
<#if hasBigDecimal>
import java.math.BigDecimal;
</#if>
import java.io.Serializable;

/**
* @author ${author}
* @date ${date}
        ${prefix}
*/
@Data
@TableName("${tableName}")
public class ${className} implements Serializable {
<#if columns??>
    <#list columns as column>

<#if column.columnKey??>
    <#if column.columnKey = 'PRI'>
        @TableId(value = "id", type = IdType.AUTO)
    </#if>
</#if>
    @TableField( "${column.columnName}")
    private ${column.columnType} ${column.changeColumnName};
    </#list>
</#if>


}
