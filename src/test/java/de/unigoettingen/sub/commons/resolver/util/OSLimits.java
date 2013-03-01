/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unigoettingen.sub.commons.resolver.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cmahnke
 */
public class OSLimits {

    private POSIX posix;
    private Map<RLIMIT_ENUM, Rlimit> limits = new HashMap<RLIMIT_ENUM, Rlimit>();
    //This works for BSDs like OSX
    private long RLIM_INFINITY = (1l << 63) - 1;
    
    public OSLimits() {
        this.posix = (POSIX) Native.loadLibrary("c", POSIX.class);

        //TODO: This doesn't work since it is set during compile time
        /*
        NativeLibrary lib = NativeLibrary.getInstance("c");
        Pointer p = lib.getGlobalVariableAddress("RLIM_INFINITY");

        RLIM_INFINITY = p.getLong(0);
        */
    }

    /**
     * See
     * http://freebsd.active-venture.com/FreeBSD-srctree/newsrc/sys/resource.h.html
     * or
     * http://linux.die.net/man/2/setrlimit
     */
    public enum RLIMIT_ENUM {

        /**
         * cpu time in milliseconds
         */
        RLIMIT_CPU(0),
        /**
         * maximum file size
         */
        RLIMIT_FSIZE(1),
        /**
         * data size
         */
        RLIMIT_DATA(2),
        /**
         * stack size
         */
        RLIMIT_STACK(3),
        /**
         * core file size
         */
        RLIMIT_CORE(4),
        /**
         * resident set size
         */
        RLIMIT_RSS(5),
        /**
         * locked-in-memory address space
         */
        RLIMIT_MEMLOCK(6),
        /**
         * number of processes
         */
        RLIMIT_NPROC(7),
        /**
         * number of open files
         */
        RLIMIT_NOFILE(8),
        /**
         * maximum size of all socket buffers
         */
        RLIMIT_SBSIZE(9);
        private final int limit;

        RLIMIT_ENUM(int limit) {
            this.limit = limit;
        }

        public int getValue() {
            return this.limit;
        }
    }

    public class Rlimit extends Structure {

        public Rlimit() {
        }

        /**
         * Simple constructor that take a value for soft and hard limit.
         *
         * @param value
         */
        public Rlimit(long value) {
            rlim_cur = value;
            rlim_max = value;
        }
        /**
         * current (soft) limit
         */
        public long rlim_cur;
        /**
         * maximum value for rlim_cur
         */
        public long rlim_max;
    }

    protected interface POSIX extends Library {

        public int getrlimit(int resource, Rlimit rlp);

        public int setrlimit(int resource, Rlimit rlp);
    }

    private void get(RLIMIT_ENUM limit) {
        if (!this.limits.containsKey(limit)) {
            Rlimit lim = new Rlimit();
            int result = posix.getrlimit(limit.getValue(), lim);
            if (result != 0) {
                throw new IllegalStateException("rlimit call failed");
            }
            this.limits.put(limit, lim);
        }
    }

    private boolean set(RLIMIT_ENUM limit, int soft, int hard) {
        Rlimit lim = new Rlimit();
        lim.rlim_cur = soft;
        lim.rlim_max = hard;
        int result = posix.setrlimit(limit.getValue(), lim);
        if (result != 0) {
            return false;
        }
        return true;
    }

    public Long getSoftLimit(RLIMIT_ENUM limit) {
        get(limit);
        return limits.get(limit).rlim_cur;
    }

    public Long getHardLimit(RLIMIT_ENUM limit) {
        get(limit);
        return limits.get(limit).rlim_max;
    }

    public long getRLIM_INFINITY() {
        return RLIM_INFINITY;
    }

    public boolean isInfinity(long val) {
        if (val == RLIM_INFINITY) {
            return true;
        }
        return false;
    }
}
