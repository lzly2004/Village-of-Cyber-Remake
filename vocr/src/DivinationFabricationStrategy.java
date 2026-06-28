public enum DivinationFabricationStrategy
{
    WOLF(7) {
        @Override
        public int adjustTarget(int option, int target, int targetRole, int n)
        {
            if (option == 1 && targetRole == 7
                    && !GameLogicUtils.probabilityJudge(80))
                return target - n;
            return target;
        }
    },
    MADMAN(8) {
        @Override
        public int adjustTarget(int option, int target, int targetRole, int n)
        {
            if (option == 0 && GameLogicUtils.probabilityJudge(15))
                return target + n;
            return target;
        }
    },
    FANATIC(9) {
        @Override
        public int adjustTarget(int option, int target, int targetRole, int n)
        {
            if (option == 1 && targetRole == 7
                    && !GameLogicUtils.probabilityJudge(90))
                return target - n;
            return target;
        }
    },
    FOX(10) {
        @Override
        public int adjustTarget(int option, int target, int targetRole, int n)
        {
            if (option == 1 && GameLogicUtils.probabilityJudge(15))
                return target - n;
            return target;
        }
    },
    DEVIANT(11) {
        @Override
        public int adjustTarget(int option, int target, int targetRole, int n)
        {
            int result = target;
            if (option == 1 && targetRole == 10)
                result -= n;
            if (option == 0 && GameLogicUtils.probabilityJudge(10))
                result += n;
            return result;
        }
    };

    private final int roleCode;

    DivinationFabricationStrategy(int roleCode)
    {
        this.roleCode = roleCode;
    }

    public abstract int adjustTarget(int option, int target, int targetRole, int n);

    public static DivinationFabricationStrategy forRole(int roleCode)
    {
        for (DivinationFabricationStrategy s : values())
            if (s.roleCode == roleCode)
                return s;
        return null;
    }
}
