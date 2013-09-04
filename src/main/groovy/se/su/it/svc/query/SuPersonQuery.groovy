/*
 * Copyright (c) 2013, IT Services, Stockholm University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Stockholm University nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package se.su.it.svc.query

import groovy.util.logging.Slf4j
import se.su.it.svc.ldap.SuPerson

/**
 * This class is a helper class for doing GLDAPO queries on the SuPerson GLDAPO schema.
 */
@Slf4j
public class SuPersonQuery {

  /**
   * Returns a SuPerson object, specified by the parameter uid.
   *
   *
   * @param directory which directory to use, see GldapoManager.
   * @param uid  the uid (user id) for the user that you want to find.
   * @return an <code><SuPerson></code> or null.
   * @see se.su.it.svc.ldap.SuPerson
   * @see se.su.it.svc.manager.GldapoManager
   */
  static SuPerson getSuPersonFromUID(String directory, String uid) {
    SuPerson suPerson = null
    try {
      suPerson = SuPerson.find(directory: directory, base: "") {
        and {
          eq("uid", uid)
          eq("objectclass", "suPerson")
        }
      }
    } catch (ex) {
      log.error "Failed finding SuPerson for uid: $uid", ex
    }

    return suPerson
  }

  /**
   * Finds all SuPerson objects, specified by the parameter ssn.
   *
   * @param directory which directory to use, see GldapoManager.
   * @param ssn  the ssn (social security number) for the user that you want to find.
   * @return a array of <code><SuPerson></code> or null.
   * @see se.su.it.svc.ldap.SuPerson
   * @see se.su.it.svc.manager.GldapoManager
   */
  static List<SuPerson> getSuPersonFromSsn(String directory, String ssn) {
    return SuPerson.findAll(directory: directory, base: "") {
      and {
        eq("socialSecurityNumber", ssn)
        eq("objectclass", "person")
      }
    }
  }

  /**
   * Save a SuPerson object to ldap.
   * and putting the changed object in the cache so that the objects returned by this svc is always up-to-date.
   *
   * @return void.
   * @see se.su.it.svc.ldap.SuPerson
   * @see se.su.it.svc.manager.GldapoManager
   */
  static void saveSuPerson(SuPerson person) {
    person.save()
  }

  /**
   * Init SuPerson entry in sukat
   *
   * @param directory which directory to use, see GldapoManager.
   * @param suInitPerson a SuInitPerson object to be saved in SUKAT.
   * @return void.
   * @see se.su.it.svc.ldap.SuInitPerson
   * @see se.su.it.svc.manager.GldapoManager
   */
  static void initSuPerson(String directory, SuPerson suPerson) {
    suPerson.directory = directory
    suPerson.save()
  }

  /**
   * Move a SuPerson object to a new location.
   *
   * @return void.
   * @see se.su.it.svc.ldap.SuPerson
   */
  static void moveSuPerson(SuPerson person, String parentDn) {
    if (parentDn && person?.uid) {
      person?.move("uid=${person.uid},${parentDn}")
    }
  }
}
