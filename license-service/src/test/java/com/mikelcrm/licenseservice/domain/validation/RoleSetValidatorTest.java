package com.mikelcrm.licenseservice.domain.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleSetValidatorTest {

    @Nested
    class AllRolesValidTests {

        @Test
        void allValidRoles_returnsTrue() {
            List<String> roles = List.of("tecnico", "admin", "manager");
            assertThat(RoleSetValidator.allRolesValid(roles)).isTrue();
        }

        @Test
        void singleValidRole_returnsTrue() {
            assertThat(RoleSetValidator.allRolesValid(List.of("superusuario"))).isTrue();
        }

        @Test
        void allFiveValidRoles_returnsTrue() {
            List<String> roles = List.of("tecnico", "asistente", "manager", "admin", "superusuario");
            assertThat(RoleSetValidator.allRolesValid(roles)).isTrue();
        }

        @Test
        void invalidRole_returnsFalse() {
            List<String> roles = List.of("tecnico", "invalid_role");
            assertThat(RoleSetValidator.allRolesValid(roles)).isFalse();
        }

        @Test
        void emptyList_returnsTrue() {
            assertThat(RoleSetValidator.allRolesValid(Collections.emptyList())).isTrue();
        }

        @Test
        void nullList_returnsFalse() {
            assertThat(RoleSetValidator.allRolesValid(null)).isFalse();
        }

        @Test
        void caseSensitive_upperCase_returnsFalse() {
            List<String> roles = List.of("Admin", "Tecnico");
            assertThat(RoleSetValidator.allRolesValid(roles)).isFalse();
        }
    }

    @Nested
    class IsNonEmptyTests {

        @Test
        void nonEmptyList_returnsTrue() {
            assertThat(RoleSetValidator.isNonEmpty(List.of("admin"))).isTrue();
        }

        @Test
        void multipleElements_returnsTrue() {
            assertThat(RoleSetValidator.isNonEmpty(List.of("admin", "tecnico"))).isTrue();
        }

        @Test
        void emptyList_returnsFalse() {
            assertThat(RoleSetValidator.isNonEmpty(Collections.emptyList())).isFalse();
        }

        @Test
        void nullList_returnsFalse() {
            assertThat(RoleSetValidator.isNonEmpty(null)).isFalse();
        }
    }

    @Nested
    class AreRoleSetsEqualTests {

        @Test
        void sameOrder_returnsTrue() {
            List<String> a = List.of("admin", "tecnico");
            List<String> b = List.of("admin", "tecnico");
            assertThat(RoleSetValidator.areRoleSetsEqual(a, b)).isTrue();
        }

        @Test
        void differentOrder_returnsTrue() {
            List<String> a = List.of("admin", "tecnico", "manager");
            List<String> b = List.of("manager", "admin", "tecnico");
            assertThat(RoleSetValidator.areRoleSetsEqual(a, b)).isTrue();
        }

        @Test
        void differentElements_returnsFalse() {
            List<String> a = List.of("admin", "tecnico");
            List<String> b = List.of("admin", "manager");
            assertThat(RoleSetValidator.areRoleSetsEqual(a, b)).isFalse();
        }

        @Test
        void differentSizes_returnsFalse() {
            List<String> a = List.of("admin", "tecnico");
            List<String> b = List.of("admin");
            assertThat(RoleSetValidator.areRoleSetsEqual(a, b)).isFalse();
        }

        @Test
        void duplicatesIgnored_treatedAsSet() {
            List<String> a = Arrays.asList("admin", "admin", "tecnico");
            List<String> b = List.of("admin", "tecnico");
            assertThat(RoleSetValidator.areRoleSetsEqual(a, b)).isTrue();
        }

        @Test
        void bothNull_returnsTrue() {
            assertThat(RoleSetValidator.areRoleSetsEqual(null, null)).isTrue();
        }

        @Test
        void oneNull_returnsFalse() {
            assertThat(RoleSetValidator.areRoleSetsEqual(List.of("admin"), null)).isFalse();
            assertThat(RoleSetValidator.areRoleSetsEqual(null, List.of("admin"))).isFalse();
        }

        @Test
        void bothEmpty_returnsTrue() {
            assertThat(RoleSetValidator.areRoleSetsEqual(
                    Collections.emptyList(), Collections.emptyList())).isTrue();
        }
    }
}
