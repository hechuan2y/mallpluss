package com.zscat.mallplus.sys.service.impl;


import com.zscat.mallplus.sys.mapper.GeneratorMapper;
import com.zscat.mallplus.sys.service.GeneratorService;
import com.zscat.mallplus.util.GenUtils1;
import com.zscat.mallplus.utils.ValidatorUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;


@Service
public class GeneratorServiceImpl implements GeneratorService {
    @Autowired
    GeneratorMapper generatorMapper;

    @Override
    public List<Map<String, Object>> list(String tableName) {
        if (ValidatorUtils.empty(tableName)){
            return generatorMapper.list();
        }
        List<Map<String, Object>> list = generatorMapper.list(tableName);
        return list;
    }

    @Override
    public byte[] generatorCode(String[] tableNames) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        for (String tableName : tableNames) {
            //查询表信息
            Map<String, String> table = generatorMapper.get(tableName);
            //查询列信息
            List<Map<String, String>> columns = generatorMapper.listColumns(tableName);
            //生成代码
            GenUtils1.generatorCode(table, columns, zip);
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

}
