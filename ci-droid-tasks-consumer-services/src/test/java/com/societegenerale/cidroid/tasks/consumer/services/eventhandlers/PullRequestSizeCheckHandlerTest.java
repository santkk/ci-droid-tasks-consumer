package com.societegenerale.cidroid.tasks.consumer.services.eventhandlers;

import com.societegenerale.cidroid.tasks.consumer.services.RemoteGitHub;
import com.societegenerale.cidroid.tasks.consumer.services.ResourceFetcher;
import com.societegenerale.cidroid.tasks.consumer.services.model.Message;
import com.societegenerale.cidroid.tasks.consumer.services.model.github.*;
import com.societegenerale.cidroid.tasks.consumer.services.notifiers.Notifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class PullRequestSizeCheckHandlerTest {

    private final String REPO_FULL_NAME = "someOrg/someRepo";

    private final ResourceFetcher mockResourceFetcher = mock(ResourceFetcher.class);

    private final Notifier mockNotifier = mock(Notifier.class);

    private final RemoteGitHub mockRemoteGitHub = mock(RemoteGitHub.class);

    private final Map<String, String> patternToContentMapping = new HashMap<>();

    private final int maxFilesInPR = 5;

    private final String maxFilesInPRExceededWarningMessage = "The PR should not have more than {0} files";

    private final PullRequestSizeCheckHandler handler = new PullRequestSizeCheckHandler(singletonList(mockNotifier),
            mockRemoteGitHub, mockResourceFetcher, maxFilesInPR, maxFilesInPRExceededWarningMessage);

    private final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

    private final Repository repository = new Repository();

    private PullRequestEvent pullRequestEvent;

    @BeforeEach
    public void setUp() {

        when(mockResourceFetcher.fetch("http://someUrl/liquibaseBestPractices.md")).thenReturn(Optional.of("careful with Liquibase changes !"));
        repository.setFullName(REPO_FULL_NAME);
        pullRequestEvent = new PullRequestEvent("created", 123, repository);

    }


    @Test
    public void shouldNotifyWithConfiguredContentWhenTheNumberOfFilesInPRExceedsConfiguredValue() {
        returnMoreThanConfiguredMaxFilesInPRFilesWhenFetchPullRequestFiles();

        handler.handle(pullRequestEvent);

        verify(mockNotifier, times(1)).notify(any(User.class), messageCaptor.capture(), any(Map.class));
        assertThat(messageCaptor.getValue().getContent()).contains("The PR should not have more than 5 files");

    }

    @Test
    public void shouldNotCreateTheMaxFileInPRCommentIfAlreadyCommented() {
        returnMoreThanConfiguredMaxFilesInPRFilesWhenFetchPullRequestFiles();
        returnExistingComment("The PR should not have more than 5 files");

        handler.handle(pullRequestEvent);

        verify(mockNotifier, never()).notify(any(User.class), messageCaptor.capture(), any(Map.class));

    }

    private void returnExistingComment(String content) {
        PullRequestComment existingPrComment = new PullRequestComment(content,
                new User("someLogin", "firstName.lastName@domain.com"));

        List<PullRequestComment> existingPRcomments = singletonList(existingPrComment);
        when(mockRemoteGitHub.fetchPullRequestComments(REPO_FULL_NAME, 123)).thenReturn(existingPRcomments);
    }


    private void returnMoreThanConfiguredMaxFilesInPRFilesWhenFetchPullRequestFiles() {
        when(mockRemoteGitHub.fetchPullRequestFiles(REPO_FULL_NAME, 123)).thenReturn(
                asList(createPullRequestFile("MyClass1.java"), createPullRequestFile("MyClass2.java")
                        , createPullRequestFile("MyClass3.java"), createPullRequestFile("MyClass4.java")
                        , createPullRequestFile("MyClass5.java"), createPullRequestFile("MyClass6.java")));
    }

    private PullRequestFile createPullRequestFile(String fileName) {
        PullRequestFile pullRequestFile = new PullRequestFile();
        pullRequestFile.setFilename(fileName);
        return pullRequestFile;
    }

}