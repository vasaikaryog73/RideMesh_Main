package com.project.ridemesh.dto;

public class PointDto {
    private double[] coordinates;
    private String type = "Point";

    public PointDto() {
    }

    public PointDto(double[] coordinates) {
        this.coordinates = coordinates;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
