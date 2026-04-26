package com.hieu.catalog_service.application.command.category;

import com.hieu.catalog_service.application.common.Command;

public record DeleteCategoryCommand(Long categoryId, String deletedBy) implements Command<Void> {}
