package magicgoose.loudnessfix;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import eu.chainfire.libsuperuser.Shell;

@SuppressWarnings("SameParameterValue")
public class MainActivity extends Activity implements Runnable {

    final private LinkedBlockingQueue<Callable<Boolean>> commands = new LinkedBlockingQueue<>();
    private final Handler mt = new Handler(Looper.getMainLooper());
    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logView = findViewById(R.id.log);
        new Thread(this).start();
    }

    public void onCheckRootClick(View view) {
        putTask(this::checkRoot);
    }

    private boolean checkRoot() {
        final boolean available = Shell.SU.available();
        log(available ? "Root check OK!" : "Root check failed");
        if (available) {
            mt.post(() -> {
                findViewById(R.id.button_patch).setEnabled(true);
                findViewById(R.id.button_check_root).setEnabled(false);
            });
        }
        return true;
    }

    private boolean doPatch() {
        final CommandResult commandResult = runCommand("cat /system/build.prop");
        check(commandResult.exitCode == 0);
        if (checkIfAlreadyPatched(commandResult.lines)) {
            log("`/system/build.prop` was already patched!");
            return false;
        }
        try {
            remountSystem(true);
            final CommandResult patchCmdResult = runCommand("printf \"\\naudio.safemedia.bypass=true\\n\" >> /system/build.prop");
            check(patchCmdResult.exitCode == 0);
            log("`/system/build.prop` successfully patched!");
        } finally {
            remountSystem(false);
        }
        return false;
    }

    private void remountSystem(boolean rw) {
        final CommandResult commandResult = runCommand("toybox mount -o " +
                (rw ? "rw" : "ro") +
                ",remount /system");
        check(commandResult.exitCode == 0);
    }

    private boolean checkIfAlreadyPatched(List<String> lines) {
        for (String line : lines) {
            if (line.contains("audio.safemedia.bypass=true")) {
                return true;
            }
        }
        return false;
    }

    private void check(boolean b) {
        if (!b) throw new IllegalStateException();
    }

    private CommandResult runCommand(String command) {
        final int[] _exitCode = new int[1];
        final List<String>[] _output = new List[1];
        final boolean[] _done = {false};
        final Shell.OnCommandResultListener listener = (int commandCode, int exitCode, List<String> output) -> {
            if (_done[0]) {
                throw new IllegalStateException();
            }
            _done[0] = true;
            _exitCode[0] = exitCode;
            _output[0] = output;
        };
        final Shell.Interactive shell = new Shell.Builder()
                .useSU()
                .setAutoHandler(false)
                .addCommand(command, 0, listener)
                .open();
        shell.waitForIdle();
        if (!_done[0]) {
            throw new IllegalStateException();
        }
        return new CommandResult(_exitCode[0], _output[0]);
    }

    private void log(String s) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            mt.post(() -> log(s));
            return;
        }
        logView.append(s);
        logView.append("\n");
    }

    private void putTask(Callable<Boolean> cmd) {
        try {
            commands.put(cmd);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void onPatchClick(View view) {
        view.setEnabled(false);
        putTask(this::doPatch);
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                final Callable<Boolean> cmd = commands.take();
                final boolean result = cmd.call();
                if (!result) break;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
