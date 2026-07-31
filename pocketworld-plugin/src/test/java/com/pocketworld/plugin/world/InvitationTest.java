package com.pocketworld.plugin.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InvitationTest {

    @Test
    void convenienceConstructorStampsCurrentTime() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        long before = System.currentTimeMillis();
        Invitation invitation = new Invitation(sender, recipient);
        long after = System.currentTimeMillis();

        assertEquals(sender, invitation.sender());
        assertEquals(recipient, invitation.recipient());
        assertEquals(true, invitation.timestamp() >= before && invitation.timestamp() <= after);
    }

    @Test
    void senderAndRecipientAreNotInterchangeable() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        Invitation invitation = new Invitation(a, b, 0L);

        assertEquals(a, invitation.sender());
        assertEquals(b, invitation.recipient());
        assertNotEquals(invitation.sender(), invitation.recipient());
    }
}
