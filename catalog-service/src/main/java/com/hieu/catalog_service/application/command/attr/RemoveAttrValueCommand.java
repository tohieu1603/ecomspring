package com.hieu.catalog_service.application.command.attr;

import com.hieu.catalog_service.application.common.Command;

public record RemoveAttrValueCommand(Long attrId, Long valId) implements Command<Void> {}
