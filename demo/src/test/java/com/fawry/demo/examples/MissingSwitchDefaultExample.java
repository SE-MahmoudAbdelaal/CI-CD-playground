package com.fawry.demo.examples;

/** MissingSwitchDefault: every switch needs a "default" branch. */
class MissingSwitchDefaultExample {

    String label(int status) {
        switch (status) {
            case 1:
                return "new";
            case 2:
                return "done";
        }
        return "unknown";
    }
}
