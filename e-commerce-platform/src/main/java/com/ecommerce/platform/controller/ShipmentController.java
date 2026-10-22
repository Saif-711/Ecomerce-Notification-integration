package com.ecommerce.platform.controller;

import com.ecommerce.platform.domain.Shipment;
import com.ecommerce.platform.dto.ShipmentRequest;
import com.ecommerce.platform.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipment Management", description = "APIs for managing shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @Operation(summary = "Create shipment", description = "Create a new shipment")
    public ResponseEntity<Shipment> createShipment(@Valid @RequestBody ShipmentRequest request) {
        Shipment shipment = shipmentService.createShipment(
                request.getUserId(),
                request.getOrderId(),
                request.getShippingAddress()
        );
        return ResponseEntity.ok(shipment);
    }

    @PostMapping("/{shipmentId}/ship")
    @Operation(summary = "Ship package", description = "Mark a shipment as shipped")
    public ResponseEntity<Shipment> shipPackage(@PathVariable Long shipmentId) {
        Shipment shipment = shipmentService.shipPackage(shipmentId);
        return ResponseEntity.ok(shipment);
    }

    @PostMapping("/{shipmentId}/deliver")
    @Operation(summary = "Deliver package", description = "Mark a shipment as delivered")
    public ResponseEntity<Shipment> deliverPackage(@PathVariable Long shipmentId) {
        Shipment shipment = shipmentService.deliverPackage(shipmentId);
        return ResponseEntity.ok(shipment);
    }

    @GetMapping("/{shipmentId}")
    @Operation(summary = "Get shipment", description = "Get shipment details by ID")
    public ResponseEntity<Shipment> getShipment(@PathVariable Long shipmentId) {
        Shipment shipment = shipmentService.getShipment(shipmentId);
        return ResponseEntity.ok(shipment);
    }
}
