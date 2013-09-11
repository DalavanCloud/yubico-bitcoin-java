/*
 * Copyright (C) 2013 Yubico AB.
 * All rights reserved.
 * Proprietary code owned by Yubico AB.
 * No rights to modifications or redistribution.
 */

package com.yubico.bitcoin.pcsc;

import com.google.common.io.BaseEncoding;
import com.yubico.bitcoin.api.IncorrectPINException;
import com.yubico.bitcoin.api.PinMode;
import org.hamcrest.Matchers;
import org.junit.*;

import static org.junit.Assert.*;

import javax.smartcardio.*;

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 9/10/13
 * Time: 1:57 PM
 * <p/>
 * These tests require a YubiKey NEO with the ykneo-bitcoin applet loaded, and the default PINs set.
 */
public class YkneoBitcoinPCSCTest {
    private static final String TERMINAL_NAME = "Yubikey NEO";
    private static final String userPin = "000000";
    private static final String adminPin = "00000000";

    private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

    private static Card card;
    private YkneoBitcoinPCSC neo;

    @BeforeClass
    public static void setupClass() throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();
        for (CardTerminal terminal : terminals.list()) {
            if (terminal.getName().contains(TERMINAL_NAME)) {
                card = terminal.connect("*");
            }
        }
        assertNotNull("YubiKey NEO not found!", card);
    }

    @Before
    public void setup() throws Exception {
        neo = new YkneoBitcoinPCSC(card.getBasicChannel());
    }

    @Test
    public void testSetUserPin() throws Exception {
        String newPin = "hello world";
        neo.setUserPin(userPin, newPin);

        try {
            neo.unlockUser(userPin);
            assertTrue(neo.isUserUnlocked());
        } catch (IncorrectPINException e) {
            assertEquals(2, e.getTriesRemaining());
            assertEquals(PinMode.USER, e.getPinMode());
            assertFalse(neo.isUserUnlocked());
        }

        neo.unlockUser(newPin);
        neo.setUserPin(newPin, userPin);
    }

    @Test
    public void testSetAdminPin() throws Exception {
        String newPin = "hello world";
        neo.setAdminPin(adminPin, newPin);

        try {
            neo.unlockAdmin(adminPin);
            assertTrue(neo.isAdminUnlocked());
        } catch (IncorrectPINException e) {
            assertEquals(2, e.getTriesRemaining());
            assertEquals(PinMode.ADMIN, e.getPinMode());
            assertFalse(neo.isAdminUnlocked());
        }

        neo.unlockAdmin(newPin);
        neo.setAdminPin(newPin, adminPin);
    }

    @Test
    public void testResetUserPin() throws Exception {
        neo.unlockAdmin(adminPin);
        boolean locked = false;
        int tries = 10;
        while (!locked && tries-- > 0) {
            try {
                neo.unlockUser("foobar");
            } catch (IncorrectPINException e) {
                locked = e.getTriesRemaining() == 0;
            }
        }
        assertTrue(locked);

        neo.resetUserPin(userPin);
        neo.unlockUser(userPin);
    }

    @Test
    public void testGenerateKey() throws Exception {
        neo.unlockAdmin(adminPin);
        byte[] privateKey = neo.generateMasterKeyPair(true, true);
        assertEquals(78, privateKey.length);
    }

    @Test
    public void testImportExtended() throws Exception {
        neo.unlockAdmin(adminPin);
        byte[] importKey = HEX.decode("0488ade4000000000000000000873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d50800e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35");
        neo.importExtendedKeyPair(importKey, true);
    }

    @Test
    public void testExportExtended() throws Exception {
        testImportExtended();
        String expectedPubKey = "0488b21e000000000000000000873dff81c02f525623fd1fe5167eac3a55a049de3d314bb42ee227ffed37d5080339a36013301597daef41fbe593a02cc513d0b55527ec2df1050e2e8ff49c85c2";
        String pubKey = HEX.encode(neo.exportExtendedPublicKey());
        assertEquals(expectedPubKey, pubKey);
    }

    @Test
    public void testGetHeader() throws Exception {
        testImportExtended();
        neo.unlockUser(userPin);
        String expectedHeader = "0488ade4000000000000000000";
        String header = HEX.encode(neo.getHeader());
        assertEquals(expectedHeader, header);
    }

    @Test
    public void testGetChild() throws Exception {
        testImportExtended();
        neo.unlockUser(userPin);
        String pubKey = HEX.encode(neo.getPublicKey(false, 0x80000000)); // m/0'
        String expectedPubKey = "045a784662a4a20a65bf6aab9ae98a6c068a81c52e4b032c0fb5400c706cfccc567f717885be239daadce76b568958305183ad616ff74ed4dc219a74c26d35f839";
        assertEquals(expectedPubKey, pubKey);
    }

    @Test
    public void testGetDescendant() throws Exception {
        testImportExtended();
        neo.unlockUser(userPin);
        String pubKey = HEX.encode(neo.getPublicKey(true, 0x80000000, 1, 0x80000000 | 2)); // m/0'/1/2'
        String expectedPubKey = "0357bfe1e341d01c69fe5654309956cbea516822fba8a601743a012a7896ee8dc2";
        assertEquals(expectedPubKey, pubKey);
    }

    @Test
    public void testSign() throws Exception {
        testImportExtended();
        neo.unlockUser(userPin);
        byte[] hash = new byte[32];
        byte[] signature = neo.sign(hash, 0);
        //TODO: Verify signature.
        assertThat(signature.length, Matchers.lessThanOrEqualTo(72));
    }
}
