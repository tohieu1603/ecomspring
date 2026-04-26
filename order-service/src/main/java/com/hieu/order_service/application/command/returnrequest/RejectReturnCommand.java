package com.hieu.order_service.application.command.returnrequest;

import com.hieu.order_service.application.common.Command;
import com.hieu.order_service.application.dto.ReturnRequestDTO;

public record RejectReturnCommand(Long returnRequestId, String adminNote)
        implements Command<ReturnRequestDTO> {}
