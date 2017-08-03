package magicgoose.loudnessfix;

import java.util.List;

class CommandResult {
    public final int exitCode;
    public final List<String> lines;

    public CommandResult(int exitCode, List<String> lines) {
        this.exitCode = exitCode;
        this.lines = lines;
    }
}
