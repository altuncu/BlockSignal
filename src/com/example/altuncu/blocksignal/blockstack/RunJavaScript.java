package com.example.altuncu.blocksignal.blockstack;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.blockstack.android.sdk.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;


public class RunJavaScript {
    private static final String TAG = RunJavaScript.class.getSimpleName();

    BlockstackSession _blockstackSession = null;

    Boolean fetchProfileValidateAppAddress (String username, String appAddress, String appOrigin) {

        Object[] params = new Object[] { username, appAddress, appOrigin };
        Context rhino = Context.enter();

        rhino.setOptimizationLevel(-1);
        try {
            Scriptable scope = rhino.initStandardObjects();
            Reader reader = new FileReader("pubKeyVerification.js");
            rhino.evaluateReader(scope, reader, "pubKeyVerification.js", 1, null);

            Object obj = scope.get("fetchProfileValidateAppAddress", scope);

            if (obj instanceof Function) {
                Function jsFunction = (Function) obj;
                jsFunction.call(rhino, scope, scope, params);
                //String result = Context.toString(jsResult);
            }
        } catch (FileNotFoundException f) {
            Log.w(TAG, f);
        }
        catch (IOException i) {
            Log.w(TAG, i);
        }
        finally {
            Context.exit();
        }

        return true;
    }
}
