package com.mikelcrm.licenseservice.domain.validation;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class for validating role sets in the funcion-roles module.
 * Provides static methods to check role membership, non-emptiness,
 * and order-independent equality of role lists.
 */
public final class RoleSetValidator {

    public static final Set<String> VALID_ROLES = Set.of(
            "tecnico", "asistente", "manager", "admin", "superusuario"
    );

    private RoleSetValidator() {
        // Utility class — prevent instantiation
    }

    /**
     * Returns true only if every element in the list is a member of VALID_ROLES.
     *
     * @param roles the list of roles to validate
     * @return true if all roles are valid, false otherwise
     */
    public static boolean allRolesValid(List<String> roles) {
        if (roles == null) {
            return false;
        }
        return roles.stream().allMatch(VALID_ROLES::contains);
    }

    /**
     * Returns true if the list is non-null and non-empty.
     *
     * @param roles the list to check
     * @return true if the list contains at least one element
     */
    public static boolean isNonEmpty(List<String> roles) {
        return roles != null && !roles.isEmpty();
    }

    /**
     * Compares two role lists as sorted sets for order-independent equality.
     * Two lists are considered equal if they contain the same elements
     * regardless of order or duplicates.
     *
     * @param a the first role list
     * @param b the second role list
     * @return true if both lists represent the same set of roles
     */
    public static boolean areRoleSetsEqual(List<String> a, List<String> b) {
        if (a == null || b == null) {
            return a == b;
        }
        Set<String> setA = new TreeSet<>(a);
        Set<String> setB = new TreeSet<>(b);
        return setA.equals(setB);
    }
}
