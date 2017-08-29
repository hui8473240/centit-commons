package com.centit.support.database.orm;

import com.centit.support.database.metadata.SimpleTableField;
import com.centit.support.database.metadata.SimpleTableReference;
import com.centit.support.database.metadata.TableInfo;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by codefan on 17-8-27.
 */
public class JpaMetadata {
    public static final ConcurrentHashMap<String , TableMapInfo> jpaMetaData =
            new ConcurrentHashMap<>(100);

    public static TableMapInfo fetchTableMapInfo(Class<?> type){
        String className = type.getName();
        TableMapInfo mapInfo = jpaMetaData.get(className);
        if(mapInfo == null){
            mapInfo = obtainMapInfoFromClass(type);
            if(mapInfo!=null){
                jpaMetaData.put(className,mapInfo);
            }
        }
        return mapInfo;
    }

    private static SimpleTableField obtainColumnFromFile(Field field){
        SimpleTableField column = new SimpleTableField();
        Column colInfo = field.getAnnotation(Column.class);
        column.setColumnName( column.getColumnName());
        column.setColumnType( column.getColumnType());
        column.setJavaType(field.getType());
        column.setPropertyName( field.getName());
        column.setMaxLength( colInfo.length());
        column.setScale( colInfo.scale());
        column.setPrecision( colInfo.precision());

        return column;
    }

    public static TableMapInfo obtainMapInfoFromClass(Class<?> objType){

        if(objType.isAnnotationPresent( Table.class ))
            return null;
        Table tableInfo = objType.getAnnotation(Table.class);
        TableMapInfo mapInfo = new TableMapInfo();
        mapInfo.setTableName( tableInfo.name() );
        mapInfo.setSchema( tableInfo.schema());

        Field[] objFields = objType.getDeclaredFields();
        for(Field field :objFields){
            if(field.isAnnotationPresent(Column.class)){
                SimpleTableField column = obtainColumnFromFile(field);
                mapInfo.addColumn(column);
                if(field.isAnnotationPresent(Id.class) ){
                    mapInfo.addPkColumns(column.getPropertyName());
                }
                if(field.isAnnotationPresent(ValueGenerator.class) ){
                    ValueGenerator valueGenerator = field.getAnnotation(ValueGenerator.class);
                    mapInfo.addValueGenerator(
                            column.getPropertyName(),
                            valueGenerator);
                }
            }else if(field.isAnnotationPresent(EmbeddedId.class)){
                EmbeddedId embeddedId = field.getAnnotation(EmbeddedId.class);
                mapInfo.setPkName(field.getName());
                for(Field idField : field.getType().getDeclaredFields()){

                    if(idField.isAnnotationPresent(Column.class)) {
                        SimpleTableField column = obtainColumnFromFile(field);
                        mapInfo.addColumn(column);
                        mapInfo.addPkColumns(column.getPropertyName());
                        if(field.isAnnotationPresent(ValueGenerator.class) ){
                            ValueGenerator valueGenerator = field.getAnnotation(ValueGenerator.class);
                            mapInfo.addValueGenerator(
                                    column.getPropertyName(),
                                    valueGenerator);
                        }
                    }
                }
            }else if(field.isAnnotationPresent(JoinColumn.class)){
                JoinColumn joinColumn =  field.getAnnotation(JoinColumn.class);
                SimpleTableReference reference = new SimpleTableReference();
                reference.setReferenceName( field.getName() );
                reference.setReferenceType( field.getType() );
                reference.addReferenceColumn( joinColumn.name(),joinColumn.referencedColumnName());
                mapInfo.addReference(reference);
            }else if(field.isAnnotationPresent(JoinColumns.class)){
                JoinColumns joinColumns =  field.getAnnotation(JoinColumns.class);
                SimpleTableReference reference = new SimpleTableReference();
                reference.setReferenceName(field.getName());
                reference.setReferenceType(field.getType());
                for(JoinColumn joinColumn : joinColumns.value()) {
                    reference.addReferenceColumn(joinColumn.name(), joinColumn.referencedColumnName());
                }
                mapInfo.addReference(reference);
            }
        }

        return mapInfo;
    }

}