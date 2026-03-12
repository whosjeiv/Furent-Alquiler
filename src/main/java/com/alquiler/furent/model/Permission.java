package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "permissions")
public class Permission {

    @Id
    private String id;
    private String roleName; // USER, MANAGER, ADMIN, SUPER_ADMIN
    private List<String> permissions = new ArrayList<>();
    private String tenantId;

    public Permission() {}

    public Permission(String roleName, List<String> permissions) {
        this.roleName = roleName;
        this.permissions = permissions;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
