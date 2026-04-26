package com.hieu.catalog_service.application.command.attr;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.AttrDTO;

public record UpdateAttrCommand(
        Long attrId,
        String name,
        String type,
        Integer sortOrder
) implements Command<AttrDTO> {}
