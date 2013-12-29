/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010, 2011 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.skmcl.swing;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.sk89q.skmcl.Launcher;
import com.sk89q.skmcl.concurrent.ExecutorWorkerService;
import com.sk89q.skmcl.concurrent.SwingProgressObserver;
import com.sk89q.skmcl.session.Account;
import com.sk89q.skmcl.session.AuthenticationException;
import com.sk89q.skmcl.session.Identity;
import com.sk89q.skmcl.session.YggdrasilSession;
import com.sk89q.skmcl.util.Persistence;
import com.sk89q.skmcl.util.SwingExecutor;
import lombok.Getter;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.sk89q.skmcl.util.SharedLocale._;

public class LoginController extends LoginDialog {

    private final ExecutorWorkerService executor = new ExecutorWorkerService(
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    @Getter private Identity identity;

    public LoginController(Window owner, Launcher launcher) {
        super(owner, launcher.getAccounts());
        new SwingProgressObserver(this, executor);
    }

    @Override
    protected void attemptLogin(Account account, String password) {
        ListenableFuture<Identity> future =
                executor.submit(
                        new LoginCallable(account, password),
                        _("loginDialog.loginTitle"),
                        _("loginDialog.loginStatus"));

        Futures.addCallback(future, new FutureCallback<Identity>() {
            @Override
            public void onSuccess(Identity result) {
                setResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, SwingExecutor.INSTANCE);

        SwingHelper.addErrorDialogCallback(future, this);
    }

    private void setResult(Identity identity) {
        this.identity = identity;
        dispose();
    }

    private class LoginCallable implements Callable<Identity> {
        private final Account account;
        private final String password;

        private LoginCallable(Account account, String password) {
            this.account = account;
            this.password = password;
        }

        @Override
        public Identity call() throws AuthenticationException, IOException, InterruptedException {
            YggdrasilSession session = new YggdrasilSession(account.getId());
            session.setPassword(password);
            session.verify();

            List<Identity> identities = session.getIdentities();

            // The list of identities (profiles in Mojang terms) corresponds to whether the account
            // owns the game, so we need to check that
            if (identities.size() > 0) {
                account.setIdentities(identities);
                Persistence.commitAndForget(getAccounts());
                return identities.get(0);
            } else {
                throw new AuthenticationException(
                        "Account doesn't own Minecraft",
                        _("loginDialog.minecraftNotOwned"));
            }
        }
    }

}
