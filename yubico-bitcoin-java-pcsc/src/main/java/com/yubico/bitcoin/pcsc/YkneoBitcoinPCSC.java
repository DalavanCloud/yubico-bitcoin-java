/*
 * Copyright (C) 2013 Yubico AB.
 * All rights reserved.
 * Proprietary code owned by Yubico AB.
 * No rights to modifications or redistribution.
 */

package com.yubico.bitcoin.pcsc;

import com.yubico.bitcoin.api.*;
import com.yubico.bitcoin.util.AbstractYkneoBitcoin;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;

/**
 * YkneoBitcoin implementation that uses javax.smartcardio to talk to a YubiKey NEO over PCSC.
 */
public class YkneoBitcoinPCSC extends AbstractYkneoBitcoin {
    private final CardChannel channel;

    public YkneoBitcoinPCSC(CardChannel channel) throws CardException, OperationInterruptedException {
        this.channel = channel;

        select();
    }

    @Override
    protected byte[] send(int cla, int ins, int p1, int p2, byte[] data) throws OperationInterruptedException {
        try {
            return channel.transmit(new CommandAPDU(cla, ins, p1, p2, data)).getBytes();
        } catch (CardException e) {
            throw new OperationInterruptedException(e);
        }
    }
}