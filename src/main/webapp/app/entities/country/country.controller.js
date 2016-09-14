(function() {
    'use strict';

    angular
        .module('jhipsterApp')
        .controller('CountryController', CountryController);

    CountryController.$inject = ['$scope', '$state', 'Country', 'CountrySearch'];

    function CountryController ($scope, $state, Country, CountrySearch) {
        var vm = this;
        
        vm.countries = [];
        vm.search = search;
        vm.loadAll = loadAll;

        loadAll();

        function loadAll() {
            Country.query(function(result) {
                vm.countries = result;
            });
        }

        function search () {
            if (!vm.searchQuery) {
                return vm.loadAll();
            }
            CountrySearch.query({query: vm.searchQuery}, function(result) {
                vm.countries = result;
            });
        }    }
})();
