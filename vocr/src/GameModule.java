import java.util.function.BiConsumer;

public class GameModule {
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final COManager coManager;
    private final ProbabilityCalculator probabilityCalculator;
    private final VoteSelector voteSelector;
    private final GameEndChecker gameEndChecker;
    private final ResultPresenter resultPresenter;
    private final Runnable gylogic;
    private final Runnable deliverEvents;
    private final BiConsumer<Integer, whyDie> dieaux;
    private final Runnable nightaction;
    private final GameRecordManager gameRecordManager;

    private GameModule(Builder builder) {
        this.ctx = builder.ctx;
        this.suspicion = builder.suspicion;
        this.coManager = builder.coManager;
        this.probabilityCalculator = builder.probabilityCalculator;
        this.voteSelector = builder.voteSelector;
        this.gameEndChecker = builder.gameEndChecker;
        this.resultPresenter = builder.resultPresenter;
        this.gylogic = builder.gylogic;
        this.deliverEvents = builder.deliverEvents;
        this.dieaux = builder.dieaux;
        this.nightaction = builder.nightaction;
        this.gameRecordManager = builder.gameRecordManager;
    }

    public GameContext getCtx() {
        return ctx;
    }

    public SuspicionSystem getSuspicion() {
        return suspicion;
    }

    public COManager getCoManager() {
        return coManager;
    }

    public ProbabilityCalculator getProbabilityCalculator() {
        return probabilityCalculator;
    }

    public VoteSelector getVoteSelector() {
        return voteSelector;
    }

    public GameEndChecker getGameEndChecker() {
        return gameEndChecker;
    }

    public ResultPresenter getResultPresenter() {
        return resultPresenter;
    }

    public Runnable getGylogic() {
        return gylogic;
    }

    public Runnable getDeliverEvents() {
        return deliverEvents;
    }

    public BiConsumer<Integer, whyDie> getDieaux() {
        return dieaux;
    }

    public Runnable getNightaction() {
        return nightaction;
    }

    public GameRecordManager getGameRecordManager() {
        return gameRecordManager;
    }

    public static class Builder {
        private GameContext ctx;
        private SuspicionSystem suspicion;
        private COManager coManager;
        private ProbabilityCalculator probabilityCalculator;
        private VoteSelector voteSelector;
        private GameEndChecker gameEndChecker;
        private ResultPresenter resultPresenter;
        private Runnable gylogic;
        private Runnable deliverEvents;
        private BiConsumer<Integer, whyDie> dieaux;
        private Runnable nightaction;
        private GameRecordManager gameRecordManager;

        public Builder ctx(GameContext ctx) {
            this.ctx = ctx;
            return this;
        }

        public Builder suspicion(SuspicionSystem suspicion) {
            this.suspicion = suspicion;
            return this;
        }

        public Builder coManager(COManager coManager) {
            this.coManager = coManager;
            return this;
        }

        public Builder probabilityCalculator(ProbabilityCalculator probabilityCalculator) {
            this.probabilityCalculator = probabilityCalculator;
            return this;
        }

        public Builder voteSelector(VoteSelector voteSelector) {
            this.voteSelector = voteSelector;
            return this;
        }

        public Builder gameEndChecker(GameEndChecker gameEndChecker) {
            this.gameEndChecker = gameEndChecker;
            return this;
        }

        public Builder resultPresenter(ResultPresenter resultPresenter) {
            this.resultPresenter = resultPresenter;
            return this;
        }

        public Builder gylogic(Runnable gylogic) {
            this.gylogic = gylogic;
            return this;
        }

        public Builder deliverEvents(Runnable deliverEvents) {
            this.deliverEvents = deliverEvents;
            return this;
        }

        public Builder dieaux(BiConsumer<Integer, whyDie> dieaux) {
            this.dieaux = dieaux;
            return this;
        }

        public Builder nightaction(Runnable nightaction) {
            this.nightaction = nightaction;
            return this;
        }

        public Builder gameRecordManager(GameRecordManager gameRecordManager) {
            this.gameRecordManager = gameRecordManager;
            return this;
        }

        public GameModule build() {
            return new GameModule(this);
        }
    }
}