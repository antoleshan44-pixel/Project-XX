package com.urbano.monolith.property.controller;

import com.urbano.monolith.property.dto.UnitDto;
import com.urbano.monolith.property.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UnitService unitService;

    @GetMapping("/vacant-units")
    public ResponseEntity<List<UnitDto>> getVacantPublishedUnits() {
        return ResponseEntity.ok(unitService.getVacantPublishedUnits());
    }

    @GetMapping("/units/{unitId}")
    public ResponseEntity<UnitDto> getUnitInternal(@PathVariable("unitId") UUID unitId) {
        return ResponseEntity.ok(unitService.getUnit(unitId));
    }

    @PutMapping("/units/{unitId}/status")
    public ResponseEntity<Void> updateUnitStatus(
            @PathVariable("unitId") UUID unitId,
            @RequestParam("status") String status) {
        unitService.updateStatus(unitId, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/units/{unitId}/occupy")
    public ResponseEntity<Void> occupyUnit(@PathVariable("unitId") UUID unitId) {
        unitService.occupyUnit(unitId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/units/{unitId}/vacate")
    public ResponseEntity<Void> vacateUnit(@PathVariable("unitId") UUID unitId) {
        unitService.vacateUnit(unitId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/units/{unitId}/maintenance")
    public ResponseEntity<Void> setUnitUnderMaintenance(@PathVariable("unitId") UUID unitId) {
        unitService.setUnitUnderMaintenance(unitId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/units/{id}/validate-pm")
    public ResponseEntity<Boolean> validateUnitPmAccount(
            @PathVariable("id") UUID id,
            @RequestParam("pmAccountId") UUID pmAccountId) {
        return ResponseEntity.ok(unitService.validateUnitPmAccount(id, pmAccountId));
    }
}