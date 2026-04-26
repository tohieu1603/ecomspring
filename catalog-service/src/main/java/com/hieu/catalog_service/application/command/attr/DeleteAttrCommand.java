package com.hieu.catalog_service.application.command.attr;

import com.hieu.catalog_service.application.common.Command;

public record DeleteAttrCommand(Long attrId) implements Command<Void> {}
