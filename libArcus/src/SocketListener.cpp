/*
 * This file is part of libArcus
 *
 * Copyright (C) 2015 Ultimaker b.v. <a.hiemstra@ultimaker.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#include "SocketListener.h"

#include "Socket.h"

using namespace Arcus;

SocketListener::SocketListener()
    : m_socket(nullptr)
{

}

SocketListener::~SocketListener()
{

}

Socket* SocketListener::socket() const
{
    return m_socket;
}

void SocketListener::setSocket(Socket* socket)
{
    m_socket = socket;
}
