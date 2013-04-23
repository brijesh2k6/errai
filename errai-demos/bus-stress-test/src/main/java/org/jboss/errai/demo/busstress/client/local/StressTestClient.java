package org.jboss.errai.demo.busstress.client.local;

import javax.annotation.PostConstruct;

import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.bus.client.api.builder.MessageBuildSendable;
import org.jboss.errai.bus.client.api.ClientMessageBus;
import org.jboss.errai.demo.busstress.client.shared.Stats;
import org.jboss.errai.ioc.client.api.EntryPoint;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

@EntryPoint
public class StressTestClient extends Composite {

  private static StressTestClientUiBinder uiBinder = GWT.create(StressTestClientUiBinder.class);

  @UiField IntegerBox messageInterval;
  @UiField Label messageIntervalError;

  @UiField IntegerBox messageSize;
  @UiField Label messageSizeError;

  @UiField IntegerBox messageMultiplier;


  @UiField Button startButton;

  @UiHandler("startButton")
  public void onStartButtonClick(ClickEvent click) {
    restart();
  }

  @UiField Button stopButton;

  @UiHandler("stopButton")
  void onStopButtonClick(ClickEvent event) {
    stopIfRunning();
  }

  @UiField SimplePanel statusPanel;

  @UiField VerticalPanel resultsPanel;

  private ClientMessageBus bus = (ClientMessageBus) ErraiBus.get();

  private Timer sendTimer;

  /**
   * The message payload that gets sent to the server.
   */
  private String messageValue;

  interface StressTestClientUiBinder extends UiBinder<Widget, StressTestClient> {
  }

  @PostConstruct
  private void init() {
    initWidget(uiBinder.createAndBindUi(this));

    BusStatusWidget busStatusWidget = new BusStatusWidget();
    bus.addLifecycleListener(busStatusWidget);
    statusPanel.add(busStatusWidget);

    RootPanel.get().add(this);
  }

  public void restart() {
    if (!validateSettings()) {
      return;
    }
    stopIfRunning();

    final Stats stats = new Stats();
    final StatsPanel statsPanel = new StatsPanel();
    resultsPanel.insert(statsPanel, 0);

    // create the message payload
    Integer messageSizeInt = messageSize.getValue();
    StringBuilder sb = new StringBuilder(messageSizeInt);
    for (int i = 0; i < messageSizeInt; i++) {
      sb.append("!");
    }
    messageValue = sb.toString();

    if (messageMultiplier.getValue() == null || messageMultiplier.getValue() < 1) {
      messageMultiplier.setValue(1);
    }

    final int multipler = messageMultiplier.getValue();

    sendTimer = new Timer() {
      private boolean hasStarted;

      @Override
      public void run() {
        hasStarted = true;

        for (int i = 0; i < multipler; i++) {
          MessageBuildSendable sendable = MessageBuilder.createMessage()
                  .toSubject("StressTestService")
                  .withValue(messageValue)
                  .done()
                  .repliesTo(new MessageCallback() {
                    @Override
                    public void callback(Message message) {
                      stats.registerReceivedMessage(message);
                      statsPanel.updateStatsLabels(stats);
                    }
                  });
          sendable.sendNowWith(bus);

          stats.registerSentMessage(sendable.getMessage());
        }

        statsPanel.updateStatsLabels(stats);
      }

      @Override
      public void cancel() {
        super.cancel();
        if (hasStarted) {
          stats.registerTestFinishing();
          statsPanel.onRunFinished(stats);
        }
      }
    };
    sendTimer.scheduleRepeating(messageInterval.getValue());

    stats.registerTestStarting();
    statsPanel.onRunStarted(stats);
  }

  private boolean validateSettings() {
    boolean valid = true;

    if (messageSize.getValue() == null) {
      valid = false;
      messageSizeError.setText("Numbers only");
      messageSize.addStyleName("error");
    }
    else {
      messageSizeError.setText("");
      messageSize.removeStyleName("error");
    }

    if (messageInterval.getValue() == null) {
      valid = false;
      messageIntervalError.setText("Numbers only");
      messageInterval.addStyleName("error");
    }
    else {
      messageIntervalError.setText("");
      messageInterval.removeStyleName("error");
    }

    return valid;
  }

  /**
   * Stops the timer if it's running. Does nothing otherwise. Safe to call any time.
   */
  void stopIfRunning() {
    if (sendTimer != null) {
      sendTimer.cancel();
      sendTimer = null;
    }
  }

}
