/*
 * Copyright (c) 2010. Axon Auction Example
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
package org.fuin.auction.command.server;

import javax.inject.Inject;
import javax.inject.Named;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.repository.AggregateNotFoundException;
import org.axonframework.repository.Repository;
import org.fuin.auction.command.api.AggregateIdResult;
import org.fuin.auction.command.api.ChangeUserPasswordCommand;
import org.fuin.auction.command.api.CommandResult;
import org.fuin.auction.command.api.EmailAlreadyExistException;
import org.fuin.auction.command.api.IdNotFoundException;
import org.fuin.auction.command.api.InvalidCommandException;
import org.fuin.auction.command.api.PasswordException;
import org.fuin.auction.command.api.RegisterUserCommand;
import org.fuin.auction.command.api.UserEmailVerificationFailedException;
import org.fuin.auction.command.api.UserIdAlreadyExistException;
import org.fuin.auction.command.api.UserIdEmailCombinationAlreadyExistException;
import org.fuin.auction.command.api.VerifyUserEmailCommand;
import org.fuin.auction.command.api.VoidSuccessResult;
import org.fuin.auction.common.Utils;
import org.fuin.objects4j.EmailAddress;
import org.fuin.objects4j.Password;
import org.fuin.objects4j.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for managing the auction commands.
 */
@Named
public class AuctionCommandHandler {

	private static final Logger LOG = LoggerFactory.getLogger(AuctionCommandHandler.class);

	@Inject
	private ConstraintSet constraintSet;

	@Inject
	@Named("userRepository")
	private Repository<User> userRepository;

	@Inject
	@IdUUID
	private AggregateIdentifierFactory userIdFactory;

	/**
	 * Sets the constraint set.
	 * 
	 * @param constraintSet
	 *            Constraint set to set.
	 */
	protected final void setConstraintSet(final ConstraintSet constraintSet) {
		this.constraintSet = constraintSet;
	}

	/**
	 * Sets the user repository.
	 * 
	 * @param userRepository
	 *            User repository to set.
	 */
	protected final void setUserRepository(final Repository<User> userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Sets the user aggregate identifier factory.
	 * 
	 * @param userIdFactory
	 *            User aggregate identifier factory to set.
	 */
	public final void setUserIdFactory(final AggregateIdentifierFactory userIdFactory) {
		this.userIdFactory = userIdFactory;
	}

	/**
	 * Register a new user.
	 * 
	 * @param command
	 *            Command to handle.
	 * 
	 * @return Result of the command.
	 */
	@CommandHandler
	public final CommandResult handle(final RegisterUserCommand command) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Handle command: " + command.toTraceString());
		}

		try {

			final UserId userId = new UserId(command.getUserId());
			final EmailAddress emailAddress = new EmailAddress(command.getEmail());
			final Password password = new Password(command.getPassword());

			constraintSet.add(userId, emailAddress);

			final User user = new User(userIdFactory.create(), userId, password, emailAddress);
			userRepository.add(user);

			return createAndLogAggregateIdResult(user.getIdentifier());

		} catch (final UserIdEmailCombinationAlreadyExistException ex) {
			LOG.error(ex.getMessage() + ": " + command.toTraceString());
			return ex.toResult();
		} catch (final UserIdAlreadyExistException ex) {
			LOG.error(ex.getMessage() + ": " + command.toTraceString());
			return ex.toResult();
		} catch (final EmailAlreadyExistException ex) {
			LOG.error(ex.getMessage() + ": " + command.toTraceString());
			return ex.toResult();
		}

	}

	/**
	 * Change the user's password.
	 * 
	 * @param command
	 *            Command to handle.
	 * 
	 * @return Result of the command.
	 */
	@CommandHandler
	public final CommandResult handle(final ChangeUserPasswordCommand command) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Handle command: " + command.toTraceString());
		}

		try {

			final AggregateIdentifier id = userIdFactory.fromString(command.getUserAggregateId());
			final Password oldPw = new Password(command.getOldPassword());
			final Password newPw = new Password(command.getNewPassword());

			final User user = userRepository.load(id);

			user.changePassword(oldPw, newPw);

			return createAndLogVoidSuccessResult();

		} catch (final PasswordException ex) {
			LOG.error(ex.getMessage() + ": " + command.toTraceString());
			return ex.toResult();
		} catch (final AggregateNotFoundException ex) {
			LOG.error(ex.getMessage() + ": " + command.toTraceString());
			return new IdNotFoundException(Utils.createMessage(ex)).toResult();
		}

	}

	/**
	 * Verify the user.
	 * 
	 * @param command
	 *            Command to handle.
	 * 
	 * @return Result of the command.
	 */
	@CommandHandler
	public final CommandResult handle(final VerifyUserEmailCommand command) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Handle command: " + command.toTraceString());
		}

		try {

			final AggregateIdentifier id = userIdFactory.fromString(command.getUserAggregateId());
			final String securityToken = command.getSecurityToken();

			final User user = userRepository.load(id);

			user.verifyEmail(securityToken);

			return createAndLogVoidSuccessResult();

		} catch (final AggregateNotFoundException ex) {
			LOG.error(ex.getMessage() + ": " + command.toTraceString());
			return new IdNotFoundException(Utils.createMessage(ex)).toResult();
		} catch (final UserEmailVerificationFailedException ex) {
			LOG.error(ex.getMessage() + ": " + command.toTraceString());
			return ex.toResult();
		} catch (final IllegalUserStateException ex) {
			LOG.error(ex.getMessage() + ": " + command.toTraceString());
			return new InvalidCommandException(ex.getMessage()).toResult();
		}

	}

	private CommandResult createAndLogVoidSuccessResult() {
		final CommandResult result = new VoidSuccessResult();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Result: " + result.toTraceString());
		}
		return result;
	}

	private CommandResult createAndLogAggregateIdResult(final AggregateIdentifier id) {
		final CommandResult result = new AggregateIdResult(id.toString());
		if (LOG.isDebugEnabled()) {
			LOG.debug("Result: " + result.toTraceString());
		}
		return result;
	}

}