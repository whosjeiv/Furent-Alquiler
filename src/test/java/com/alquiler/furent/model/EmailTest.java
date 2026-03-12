package com.alquiler.furent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void of_validEmail_createsEmail() {
        Email email = Email.of("Test@Example.COM");
        assertEquals("test@example.com", email.getValue());
    }

    @Test
    void of_withWhitespace_trimsAndNormalizes() {
        Email email = Email.of("  user@domain.com  ");
        assertEquals("user@domain.com", email.getValue());
    }

    @Test
    void of_null_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Email.of(null));
    }

    @Test
    void of_blank_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Email.of("   "));
    }

    @Test
    void of_invalidFormat_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Email.of("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> Email.of("missing@tld"));
        assertThrows(IllegalArgumentException.class, () -> Email.of("@domain.com"));
    }

    @Test
    void getDomain_returnsCorrectDomain() {
        Email email = Email.of("admin@furent.com");
        assertEquals("furent.com", email.getDomain());
    }

    @Test
    void isValid_withValidEmails_returnsTrue() {
        assertTrue(Email.isValid("user@domain.com"));
        assertTrue(Email.isValid("test+tag@example.co"));
    }

    @Test
    void isValid_withInvalidEmails_returnsFalse() {
        assertFalse(Email.isValid(null));
        assertFalse(Email.isValid(""));
        assertFalse(Email.isValid("not-email"));
    }

    @Test
    void equals_sameEmail_areEqual() {
        Email e1 = Email.of("USER@domain.com");
        Email e2 = Email.of("user@DOMAIN.com");
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void equals_differentEmail_areNotEqual() {
        Email e1 = Email.of("a@domain.com");
        Email e2 = Email.of("b@domain.com");
        assertNotEquals(e1, e2);
    }

    @Test
    void toString_returnsNormalizedValue() {
        Email email = Email.of("Test@Example.com");
        assertEquals("test@example.com", email.toString());
    }
}
