package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.User;
import com.alquiler.furent.service.UserService;
import com.alquiler.furent.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Controlador para gestión administrativa de usuarios.
 * Permite CRUD completo: listar, suspender, activar, cambiar rol, eliminar.
 */
@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuariosController {

    private final UserService userService;
    private final UserRepository userRepository;

    public AdminUsuariosController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * Lista usuarios con paginación y filtros.
     * 
     * @param rol Filtro por rol (opcional)
     * @param estado Filtro por estado (opcional)
     * @param search Búsqueda por email o nombre (opcional)
     * @param page Número de página
     * @param model Modelo para la vista
     * @return Vista de lista de usuarios
     */
    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        
        int pageSize = 20;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        
        Page<User> usersPage;
        
        // Aplicar filtros
        if (search != null && !search.trim().isEmpty()) {
            // Búsqueda por email o nombre
            usersPage = userRepository.findByEmailContainingIgnoreCaseOrNombreContainingIgnoreCase(
                    search, search, pageable);
        } else if (rol != null && !rol.isEmpty() && estado != null && !estado.isEmpty()) {
            // Filtrar por rol y estado
            usersPage = userRepository.findByRoleAndEstado(rol, estado, pageable);
        } else if (rol != null && !rol.isEmpty()) {
            // Filtrar solo por rol
            usersPage = userRepository.findByRole(rol, pageable);
        } else if (estado != null && !estado.isEmpty()) {
            // Filtrar solo por estado
            usersPage = userRepository.findByEstado(estado, pageable);
        } else {
            // Sin filtros
            usersPage = userRepository.findAll(pageable);
        }
        
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalUsers", usersPage.getTotalElements());
        model.addAttribute("selectedRol", rol);
        model.addAttribute("selectedEstado", estado);
        model.addAttribute("searchQuery", search);
        
        return "admin/usuarios";
    }

    /**
     * Suspende un usuario.
     * 
     * @param id ID del usuario
     * @param razon Razón de la suspensión
     * @param principal Usuario autenticado (admin)
     * @param redirectAttributes Atributos para mensaje flash
     * @return Redirección a lista de usuarios
     */
    @PostMapping("/{id}/suspender")
    public String suspendUser(
            @PathVariable String id,
            @RequestParam String razon,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.suspendUser(id, razon, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Usuario suspendido exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al suspender usuario: " + e.getMessage());
        }
        
        return "redirect:/admin/usuarios";
    }

    /**
     * Activa un usuario suspendido.
     * 
     * @param id ID del usuario
     * @param principal Usuario autenticado (admin)
     * @param redirectAttributes Atributos para mensaje flash
     * @return Redirección a lista de usuarios
     */
    @PostMapping("/{id}/activar")
    public String activateUser(
            @PathVariable String id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.activateUser(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Usuario activado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al activar usuario: " + e.getMessage());
        }
        
        return "redirect:/admin/usuarios";
    }

    /**
     * Cambia el rol de un usuario.
     * 
     * @param id ID del usuario
     * @param nuevoRol Nuevo rol (USER, ADMIN)
     * @param principal Usuario autenticado (admin)
     * @param redirectAttributes Atributos para mensaje flash
     * @return Redirección a lista de usuarios
     */
    @PostMapping("/{id}/rol")
    public String changeUserRole(
            @PathVariable String id,
            @RequestParam String nuevoRol,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.changeUserRole(id, nuevoRol, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Rol de usuario actualizado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar rol: " + e.getMessage());
        }
        
        return "redirect:/admin/usuarios";
    }

    /**
     * Realiza soft delete de un usuario.
     * 
     * @param id ID del usuario
     * @param principal Usuario autenticado (admin)
     * @param redirectAttributes Atributos para mensaje flash
     * @return Redirección a lista de usuarios
     */
    @DeleteMapping("/{id}")
    public String softDeleteUser(
            @PathVariable String id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            userService.softDeleteUser(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar usuario: " + e.getMessage());
        }
        
        return "redirect:/admin/usuarios";
    }
}
