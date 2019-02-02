package com.revolut.backend.unit.test;

import com.revolut.backend.utils.Args;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class ArgsTest {

    @Test
    public void testNoError(){
        Args.isTrue(1 == 1, "error msg");
    }

    @Test
    public void testError(){
        final String errorMsg = "error msg";
        try {
            Args.isTrue(1 == 2, errorMsg);
            fail();
        } catch (IllegalArgumentException e){
            assertEquals(errorMsg, e.getMessage());
        }
    }

}
