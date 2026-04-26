package com.hieu.catalog_service.interfaces.rest.dto;

import java.util.List;

public record UpdateImagesRequest(String thumbnail, List<String> images) {}
