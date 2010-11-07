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
package org.fuin.auction.client.common;

import java.net.MalformedURLException;
import java.util.UUID;

import org.fuin.auction.client.common.AuctionCmdService;
import org.fuin.auction.command.api.base.AuctionCommandService;
import org.fuin.auction.command.api.base.ChangeUserPasswordCommand;
import org.fuin.auction.command.api.base.RegisterUserCommand;
import org.fuin.auction.command.api.base.VerifyUserEmailCommand;
import org.fuin.auction.command.api.extended.IdNotFoundException;
import org.fuin.auction.command.api.extended.InternalErrorException;
import org.fuin.auction.command.api.extended.InvalidCommandException;
import org.fuin.auction.command.api.extended.PasswordException;
import org.fuin.auction.command.api.extended.UserEmailAlreadyExistException;
import org.fuin.auction.command.api.extended.UserEmailVerificationFailedException;
import org.fuin.auction.command.api.extended.UserNameAlreadyExistException;
import org.fuin.auction.command.api.extended.UserNameEmailCombinationAlreadyExistException;
import org.fuin.auction.command.api.support.AggregateIdentifierUUIDResult;
import org.fuin.auction.command.api.support.CommandResult;
import org.fuin.objects4j.Contract;
import org.fuin.objects4j.ContractViolationException;
import org.fuin.objects4j.EmailAddress;
import org.fuin.objects4j.Password;
import org.fuin.objects4j.UserName;

import com.caucho.hessian.client.HessianProxyFactory;

/**
 * Default implementation.<br>
 * <br>
 * TODO michael 20.10.2010 Generate this class on-the-fly with
 * 
 * {@link http://www.fuin.org/srcgen4javassist/index.html}
 * 
 * or replace it with some other automatic construct.
 */
public final class AuctionCmdServiceImpl implements AuctionCmdService {

	private final AuctionCommandService commandService;

	/**
	 * Constructor with factory and URL.
	 * 
	 * @param factory
	 *            Factory to use.
	 * @param url
	 *            Service URL.
	 * 
	 * @throws MalformedURLException
	 *             The URL was invalid.
	 */
	public AuctionCmdServiceImpl(final HessianProxyFactory factory, final String url)
	        throws MalformedURLException {
		super();
		commandService = (AuctionCommandService) factory.create(AuctionCommandService.class, url);

	}

	@Override
	public final UUID registerUser(final UserName userName, final Password password,
	        final EmailAddress email) throws UserNameEmailCombinationAlreadyExistException,
	        UserNameAlreadyExistException, UserEmailAlreadyExistException {

		final RegisterUserCommand cmd = new RegisterUserCommand(userName.toString(), password
		        .toString(), email.toString());

		final CommandResult result = commandService.send(cmd);
		if (result.isSuccess()) {
			final AggregateIdentifierUUIDResult rucr = (AggregateIdentifierUUIDResult) result;
			return UUID.fromString(rucr.getId());
		}

		// Error handling
		switch (result.getMessageId()) {
		case UserNameEmailCombinationAlreadyExistException.MESSAGE_ID:
			throw new UserNameEmailCombinationAlreadyExistException(result.getInternalMessage());
		case UserNameAlreadyExistException.MESSAGE_ID:
			throw new UserNameAlreadyExistException(result.getInternalMessage());
		case UserEmailAlreadyExistException.MESSAGE_ID:
			throw new UserEmailAlreadyExistException(result.getInternalMessage());
		case InternalErrorException.MESSAGE_ID:
			throw new InternalErrorException(result.getInternalMessage());
		case InvalidCommandException.MESSAGE_ID:
			throw new InvalidCommandException(result.getInternalMessage());
		default:
			throw new IllegalStateException("Unknown message id: " + result.getMessageId());
		}

	}

	@Override
	public final void changeUserPassword(final UUID userAggregateId, final Password oldPassword,
	        final Password newPassword) throws IdNotFoundException, PasswordException {

		final ChangeUserPasswordCommand cmd = new ChangeUserPasswordCommand(userAggregateId
		        .toString(), oldPassword.toString(), newPassword.toString());

		final CommandResult result = commandService.send(cmd);
		if (!result.isSuccess()) {

			// Error handling
			switch (result.getMessageId()) {
			case IdNotFoundException.MESSAGE_ID:
				throw new IdNotFoundException(result.getInternalMessage());
			case PasswordException.MESSAGE_ID:
				throw new PasswordException(result.getInternalMessage());
			case InternalErrorException.MESSAGE_ID:
				throw new InternalErrorException(result.getInternalMessage());
			case InvalidCommandException.MESSAGE_ID:
				throw new InvalidCommandException(result.getInternalMessage());
			default:
				throw new IllegalStateException("Unknown message id: " + result.getMessageId());
			}

		}

	}

	@Override
	public final void verifyUserEmail(final UUID userAggregateId, final String securityToken)
	        throws IdNotFoundException, UserEmailVerificationFailedException {

		// TODO michael 07.11.2010 Handle the checks more nicely!
		// The above arguments are a 1:1 mapping from the fields of the command
		// and so the same validations should be checked here BEFORE the command
		// is created. The error message should fit the fact that it's not a
		// command field but an argument that is checked.

		Contract.requireArgNotNull("userAggregateId", userAggregateId);
		Contract.requireArgNotNull("securityToken", securityToken);

		final VerifyUserEmailCommand cmd;
		try {
			cmd = new VerifyUserEmailCommand(userAggregateId.toString(), securityToken);
		} catch (final ContractViolationException ex) {
			throw new InvalidCommandException(ex.getMessage());
		}

		final CommandResult result = commandService.send(cmd);
		if (!result.isSuccess()) {

			// Error handling
			switch (result.getMessageId()) {
			case IdNotFoundException.MESSAGE_ID:
				throw new IdNotFoundException(result.getInternalMessage());
			case UserEmailVerificationFailedException.MESSAGE_ID:
				throw new UserEmailVerificationFailedException();
			case InternalErrorException.MESSAGE_ID:
				throw new InternalErrorException(result.getInternalMessage());
			case InvalidCommandException.MESSAGE_ID:
				throw new InvalidCommandException(result.getInternalMessage());
			default:
				throw new IllegalStateException("Unknown message id: " + result.getMessageId());
			}

		}

	}

}