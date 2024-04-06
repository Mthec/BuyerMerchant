package mod.wurmunlimited;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.FakeCommunicator;
import com.wurmonline.server.questions.NoSuchQuestionException;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.questions.Questions;
import org.junit.jupiter.api.BeforeEach;

import java.util.Properties;

public abstract class WurmTradingQuestionTest extends WurmTradingTest {

    private Question question;
    protected Properties answers;
    protected Creature sender;
    protected FakeCommunicator com;

    @BeforeEach
    void resetAnswers() {
        answers = new Properties();
    }

    protected void askQuestion(Question question) {
        this.question = question;
        question.sendQuestion();
        sender = question.getResponder();
        com = factory.getCommunicator(sender);
    }

    protected void answer() {
        question.answer(answers);
        try {
            question = Questions.getQuestion(getPassThroughId(factory.getCommunicator(sender).lastBmlContent));
        } catch (NoSuchQuestionException e) {
            throw new RuntimeException(e);
        }
        resetAnswers();
    }
}
