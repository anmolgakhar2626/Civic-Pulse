package com.smartcity.department;

import com.smartcity.common.PagedResponse;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.department.dto.DepartmentResponse;
import com.smartcity.department.dto.TeamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Departments", description = "Municipal department and operational team queries")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;

    public DepartmentController(DepartmentRepository departmentRepository, TeamRepository teamRepository) {
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
    }

    @GetMapping
    @Operation(summary = "List departments with pagination", description = "Retrieve a paginated list of municipal departments including active teams.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Departments retrieved successfully")
    })
    public ResponseEntity<PagedResponse<DepartmentResponse>> getDepartments(
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Department> deptPage = departmentRepository.findAll(pageable);

        List<DepartmentResponse> responses = deptPage.getContent().stream().map(dept -> {
            List<TeamResponse> teams = teamRepository.findByDepartmentIdAndActiveTrue(dept.getId())
                    .stream()
                    .map(TeamResponse::fromEntity)
                    .collect(Collectors.toList());
            return DepartmentResponse.fromEntity(dept, teams);
        }).collect(Collectors.toList());

        PagedResponse<DepartmentResponse> pagedResponse = new PagedResponse<>(
                responses,
                deptPage.getNumber(),
                deptPage.getSize(),
                deptPage.getTotalElements(),
                deptPage.getTotalPages(),
                deptPage.isFirst(),
                deptPage.isLast()
        );

        return ResponseEntity.ok(pagedResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Retrieve a specific department by its UUID including its teams.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department found"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<DepartmentResponse> getDepartmentById(
            @Parameter(description = "Department UUID")
            @PathVariable UUID id
    ) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        List<TeamResponse> teams = teamRepository.findByDepartmentIdAndActiveTrue(dept.getId())
                .stream()
                .map(TeamResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(DepartmentResponse.fromEntity(dept, teams));
    }
}
