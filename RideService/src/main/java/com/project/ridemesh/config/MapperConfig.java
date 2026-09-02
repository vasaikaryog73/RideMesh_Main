package com.project.ridemesh.config;

import com.project.ridemesh.dto.PointDto;
import com.project.ridemesh.utils.GeometryUtil;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper(){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.typeMap(PointDto.class, Point.class).setConverter(context -> {
            PointDto pointDto = context.getSource();
            return pointDto == null ? null : GeometryUtil.createPoint(pointDto);
        });

        modelMapper.typeMap(Point.class, PointDto.class).setConverter(mappingContext -> {
            Point point = mappingContext.getSource();
            if (point == null) return null;
            double[] coordinates = {point.getX(), point.getY()};
            return new PointDto(coordinates);
        });

        return modelMapper;
    }
}
