package com.concer.backend.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMergeData {
    private Integer userId;
    private Integer eventsId;
    private String eventsName;
    private String eventsDate;
    private Integer  totalQty;
    private Integer totalAmount;
    private String  createDate;
    private Integer status;
    private List<OrdersResponse> detail ;

}
