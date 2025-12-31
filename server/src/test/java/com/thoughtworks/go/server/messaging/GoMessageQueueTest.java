/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoMessageQueueTest {
    private static final String QUEUE = "queue";

    @Mock MessagingService<GoMessage> messagingService;
    @Mock GoMessageListener<GoMessage> listener;

    private GoMessageQueue<GoMessage> queue;

    @BeforeEach
    void setUp() {
        queue = new GoMessageQueue<>(messagingService, QUEUE);
    }

    @Test
    void shouldPassThroughBasicFunctionsToService() {
        queue.addListener(listener);
        verify(messagingService).addQueueListener(QUEUE, listener);

        queue.stop();
        verify(messagingService).removeQueue(QUEUE);
    }

    @Test
    void shouldCreateSenderLazilyAndOnlyOnce() {
        assertThat(queue.queueName).isEqualTo(QUEUE);
        verifyNoInteractions(messagingService);

        MessageSender firstSender = mock(MessageSender.class);
        when(messagingService.createQueueSender(QUEUE)).thenReturn(firstSender, mock(MessageSender.class));

        queue.post(mock(GoMessage.class));
        queue.post(mock(GoMessage.class));
        queue.post(mock(GoMessage.class), 123);

        verify(messagingService).createQueueSender(QUEUE);

        verify(firstSender, times(2)).sendMessage(any());
        verify(firstSender, times(1)).sendMessage(any(), eq(123L));
    }
}